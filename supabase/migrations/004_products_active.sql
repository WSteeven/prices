-- =============================================================
-- Prices App: products.active como papelera.
-- "Eliminar" en la app pone active = false; el producto se ve en el filtro
-- "Desactivados" y se puede reactivar. No se borran filas desde la app.
-- Ejecutar después de 003. Es idempotente.
-- =============================================================

alter table public.products
    add column if not exists active boolean;

update public.products set active = true where active is null;

alter table public.products
    alter column active set default true,
    alter column active set not null;
