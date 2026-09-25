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
