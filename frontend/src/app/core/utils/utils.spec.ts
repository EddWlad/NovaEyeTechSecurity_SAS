import { toDateInputValue, toInitials, toMoney } from './format.util';
import { MAX_UPLOAD_BYTES, imageFileError, toImageSrc } from './image.util';
import { relationLabel } from './relation.util';

describe('format.util', () => {
  it('toMoney formatea con dos decimales y tolera valores invalidos', () => {
    expect(toMoney('35.5')).toContain('35,50');
    expect(toMoney('no es numero')).toContain('0,00');
  });

  it('toDateInputValue devuelve yyyy-mm-dd o vacio', () => {
    expect(toDateInputValue('2026-09-25T10:00:00Z')).toBe('2026-09-25');
    expect(toDateInputValue('fecha rota')).toBe('');
  });

  it('toInitials toma las dos primeras palabras', () => {
    expect(toInitials('benjamín llamuca  pérez')).toBe('BL');
    expect(toInitials('')).toBe('');
  });
});

describe('toImageSrc', () => {
  it('acepta https, data URLs y completa www.', () => {
    expect(toImageSrc('https://res.cloudinary.com/x/a.png')).toBe('https://res.cloudinary.com/x/a.png');
    expect(toImageSrc('data:image/png;base64,AAA')).toBe('data:image/png;base64,AAA');
    expect(toImageSrc('  www.ejemplo.com/a.png ')).toBe('https://www.ejemplo.com/a.png');
  });

  it('devuelve null sin imagen', () => {
    expect(toImageSrc(null)).toBeNull();
    expect(toImageSrc('   ')).toBeNull();
    expect(toImageSrc(42)).toBeNull();
  });
});

describe('imageFileError', () => {
  const fileOf = (bytes: number, type: string) => {
    const file = new File([''], 'foto', { type });
    Object.defineProperty(file, 'size', { value: bytes });
    return file;
  };

  it('acepta imagenes de hasta 10 MB', () => {
    expect(imageFileError(fileOf(9.9 * 1024 * 1024, 'image/jpeg'))).toBeNull();
    expect(imageFileError(fileOf(MAX_UPLOAD_BYTES, 'image/webp'))).toBeNull();
  });

  it('rechaza las que superan 10 MB o no son imagen', () => {
    expect(imageFileError(fileOf(MAX_UPLOAD_BYTES + 1, 'image/png'))).toContain('10 MB');
    expect(imageFileError(fileOf(1024, 'application/pdf'))).toContain('Formato');
  });
});

describe('relationLabel', () => {
  const row = {
    categoryId: 'c1',
    category: { name: 'Cámaras' },
    mainSupplierId: 's1',
    mainSupplier: { businessName: 'SISEGUSA' },
    clientId: 'x',
  };

  it('lee el nombre de la relacion anidada', () => {
    expect(relationLabel(row, 'categoryId')).toBe('Cámaras');
    expect(relationLabel(row, 'mainSupplierId')).toBe('SISEGUSA');
  });

  it('devuelve null si la clave no es xxxId o la relacion no viene', () => {
    expect(relationLabel(row, 'category')).toBeNull();
    expect(relationLabel(row, 'clientId')).toBeNull();
  });
});
