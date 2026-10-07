-- =============================================================
-- Prices App: roles (admin / empleado) + Row Level Security
-- Reutiliza la tabla existente public.user_roles (user_id, role).
-- Ejecutar en Supabase > SQL Editor. Es idempotente.
-- =============================================================

-- ---------- Tabla de productos (solo si aún no existe) ----------
create table if not exists public.products (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    price       numeric(10, 2) not null check (price >= 0),
    image_url   text,
    barcode     text unique,
    active      boolean not null default true,
    created_at  timestamptz not null default now()
);

-- ---------- Roles ----------
create table if not exists public.user_roles (
    user_id  uuid primary key references auth.users (id) on delete cascade,
    role     text not null default 'empleado'
);

-- Rol del usuario actual (security definer evita recursión de RLS)
create or replace function public.get_user_role()
returns text
language sql
stable
security definer set search_path = ''
as $$
    select role from public.user_roles where user_id = (select auth.uid());
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select coalesce(public.get_user_role() = 'admin', false);
$$;

-- Todo usuario nuevo empieza como empleado
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.user_roles (user_id, role)
    select new.id, 'empleado'
    where not exists (select 1 from public.user_roles where user_id = new.id);
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Usuarios que ya existían sin rol
insert into public.user_roles (user_id, role)
select u.id, 'empleado' from auth.users u
where not exists (select 1 from public.user_roles r where r.user_id = u.id);

-- Las políticas se combinan con OR: se borran las previas de estas tablas
-- para que ninguna política vieja abra acceso extra.
do $$
declare pol record;
begin
    for pol in select tablename, policyname from pg_policies
               where schemaname = 'public' and tablename in ('products', 'user_roles')
    loop
        execute format('drop policy %I on public.%I', pol.policyname, pol.tablename);
    end loop;
    for pol in select policyname from pg_policies
               where schemaname = 'storage' and tablename = 'objects'
                 and (qual like '%product-images%' or with_check like '%product-images%')
    loop
        execute format('drop policy %I on storage.objects', pol.policyname);
    end loop;
end $$;

-- ---------- RLS: user_roles ----------
alter table public.user_roles enable row level security;

create policy "user_roles_select_own_or_admin" on public.user_roles
    for select to authenticated
    using (user_id = (select auth.uid()) or public.is_admin());

create policy "user_roles_admin_write" on public.user_roles
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------- RLS: products ----------
alter table public.products enable row level security;

create policy "products_select_authenticated" on public.products
    for select to authenticated
    using (true);

create policy "products_insert_admin" on public.products
    for insert to authenticated
    with check (public.is_admin());

create policy "products_update_authenticated" on public.products
    for update to authenticated
    using (true)
    with check (true);

create policy "products_delete_admin" on public.products
    for delete to authenticated
    using (public.is_admin());

-- El empleado solo puede cambiar imagen y código de barras
create or replace function public.products_guard_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if not public.is_admin() then
        if new.name is distinct from old.name
           or new.price is distinct from old.price
           or new.id is distinct from old.id then
            raise exception 'Solo un administrador puede cambiar nombre o precio'
                using errcode = '42501';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists products_guard_update on public.products;
create trigger products_guard_update
    before update on public.products
    for each row execute function public.products_guard_update();

-- ---------- Storage: bucket product-images ----------
insert into storage.buckets (id, name, public)
values ('product-images', 'product-images', true)
on conflict (id) do nothing;

create policy "product_images_select_authenticated" on storage.objects
    for select to authenticated
    using (bucket_id = 'product-images');

create policy "product_images_insert_authenticated" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'product-images');

create policy "product_images_update_admin" on storage.objects
    for update to authenticated
    using (bucket_id = 'product-images' and public.is_admin());

create policy "product_images_delete_admin" on storage.objects
    for delete to authenticated
    using (bucket_id = 'product-images' and public.is_admin());

-- ---------- Promover un usuario a admin ----------
-- update public.user_roles set role = 'admin'
-- where user_id = (select id from auth.users where email = 'tu-correo@ejemplo.com');
