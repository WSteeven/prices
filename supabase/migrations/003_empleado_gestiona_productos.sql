-- =============================================================
-- Prices App: el empleado también crea, edita y borra productos.
-- Solo quedan reservados al admin: roles de usuario y unidades de medida.
-- Ejecutar después de 002. Es idempotente.
-- =============================================================

drop policy if exists "products_insert_admin" on public.products;
drop policy if exists "products_insert_authenticated" on public.products;
create policy "products_insert_authenticated" on public.products
    for insert to authenticated
    with check (true);

drop policy if exists "products_delete_admin" on public.products;
drop policy if exists "products_delete_authenticated" on public.products;
create policy "products_delete_authenticated" on public.products
    for delete to authenticated
    using (true);

-- Ya no se restringe qué columnas puede cambiar el empleado
drop trigger if exists products_guard_update on public.products;
drop function if exists public.products_guard_update();
