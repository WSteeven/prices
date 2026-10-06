-- =============================================================
-- Prices App: roles (admin / empleado) + Row Level Security
-- Ejecutar en Supabase > SQL Editor. Es idempotente.
-- =============================================================

-- ---------- Tabla de productos (solo si aún no existe) ----------
create table if not exists public.products (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    price       numeric(10, 2) not null check (price >= 0),
    image_url   text,
    barcode     text unique,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

-- ---------- Perfiles con rol ----------
create table if not exists public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    email       text,
    role        text not null default 'empleado' check (role in ('admin', 'empleado')),
    created_at  timestamptz not null default now()
);

-- Crea el perfil automáticamente al registrar un usuario
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, email)
    values (new.id, new.email)
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Perfiles para usuarios que ya existían
insert into public.profiles (id, email)
select id, email from auth.users
on conflict (id) do nothing;

-- Helper: ¿el usuario actual es admin? (security definer evita recursión de RLS)
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select exists (
        select 1 from public.profiles
        where id = (select auth.uid()) and role = 'admin'
    );
$$;

-- ---------- RLS: profiles ----------
alter table public.profiles enable row level security;

drop policy if exists "profiles_select_own_or_admin" on public.profiles;
create policy "profiles_select_own_or_admin" on public.profiles
    for select to authenticated
    using (id = (select auth.uid()) or public.is_admin());

drop policy if exists "profiles_admin_update" on public.profiles;
create policy "profiles_admin_update" on public.profiles
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

-- ---------- RLS: products ----------
alter table public.products enable row level security;

-- Las políticas se combinan con OR: borramos cualquier política previa
-- (p. ej. "Enable read access for all users") para que no abra acceso extra.
do $$
declare pol record;
begin
    for pol in select policyname from pg_policies
               where schemaname = 'public' and tablename = 'products'
    loop
        execute format('drop policy %I on public.products', pol.policyname);
    end loop;
end $$;

drop policy if exists "products_select_authenticated" on public.products;
create policy "products_select_authenticated" on public.products
    for select to authenticated
    using (true);

drop policy if exists "products_insert_admin" on public.products;
create policy "products_insert_admin" on public.products
    for insert to authenticated
    with check (public.is_admin());

drop policy if exists "products_update_authenticated" on public.products;
create policy "products_update_authenticated" on public.products
    for update to authenticated
    using (true)
    with check (true);

drop policy if exists "products_delete_admin" on public.products;
create policy "products_delete_admin" on public.products
    for delete to authenticated
    using (public.is_admin());

-- El empleado solo puede cambiar imagen y código de barras
create or replace function public.products_guard_update()
returns trigger
language plpgsql
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

drop policy if exists "product_images_insert_authenticated" on storage.objects;
create policy "product_images_insert_authenticated" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'product-images');

drop policy if exists "product_images_update_admin" on storage.objects;
create policy "product_images_update_admin" on storage.objects
    for update to authenticated
    using (bucket_id = 'product-images' and public.is_admin());

drop policy if exists "product_images_delete_admin" on storage.objects;
create policy "product_images_delete_admin" on storage.objects
    for delete to authenticated
    using (bucket_id = 'product-images' and public.is_admin());

-- ---------- Promover tu usuario a admin (cambia el correo) ----------
-- update public.profiles set role = 'admin' where email = 'tu-correo@ejemplo.com';
