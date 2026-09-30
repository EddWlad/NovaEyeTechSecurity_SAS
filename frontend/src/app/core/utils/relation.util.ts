import { SelectOption } from '../models/resource.models';

/**
 * Nombre legible de una relación que el backend devuelve anidada junto a su `xxxId`.
 *
 * Para `categoryId` lee `category.name`; para `mainSupplierId`, `mainSupplier.businessName`.
 * Devuelve `null` si la clave no es un `xxxId` o la relación no viene en la respuesta.
 */
export const relationLabel = (row: Record<string, unknown>, idKey: string): string | null => {
  if (!idKey.endsWith('Id')) {
    return null;
  }

  const relation = row[idKey.replace(/Id$/, '')];
  if (!relation || typeof relation !== 'object') {
    return null;
  }

  const record = relation as Record<string, unknown>;
  const label =
    record['name'] ?? record['fullName'] ?? record['businessName'] ?? record['nameOrBusinessName'];

  return label ? String(label) : null;
};

/**
 * Etiqueta de un valor de una lista fija de opciones (el rol `ADMIN_OPERATIVO` se muestra como
 * "Administrador"). Devuelve `null` si el campo no tiene opciones o el valor no está entre ellas.
 */
export const optionLabel = (options: SelectOption[] | undefined, value: unknown): string | null =>
  options?.find((option) => option.value === value)?.label ?? null;
