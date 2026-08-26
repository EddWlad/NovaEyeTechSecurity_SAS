import {
  MaintenanceStatus,
  MaintenanceType,
  QuotationItemType,
  QuotationStatus,
  Role,
} from './enums';

export interface BaseEntity {
  id: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface User extends BaseEntity {
  email: string;
  fullName: string;
  role: Role;
  phone?: string | null;
  avatarDataUrl?: string | null;
  active: boolean;
}

export interface Client extends BaseEntity {
  nameOrBusinessName: string;
  documentNumber: string;
  phone: string;
  email?: string | null;
  address: string;
  city: string;
  commercialReference?: string | null;
  observations?: string | null;
  active: boolean;
}

export interface Supplier extends BaseEntity {
  businessName: string;
  ruc: string;
  contact: string;
  phone: string;
  email?: string | null;
  address: string;
  city: string;
  active: boolean;
}

export interface ProductCategory extends BaseEntity {
  name: string;
  description?: string | null;
  active: boolean;
}

export interface ServiceCategory extends BaseEntity {
  name: string;
  description?: string | null;
  active: boolean;
}

export interface Product extends BaseEntity {
  category: ProductCategory;
  mainSupplier?: Supplier | null;
  internalCode: string;
  name: string;
  brand: string;
  model?: string | null;
  description: string;
  baseCost: string;
  stock?: string | null;
  unit: string;
  imageUrl?: string | null;
  active: boolean;
}

export interface Service extends BaseEntity {
  category: ServiceCategory;
  name: string;
  description: string;
  baseCost: string;
  active: boolean;
}

export interface QuotationSetting extends BaseEntity {
  currentVat: string;
  allowedVatRates: string[];
  allowedMargins: string[];
  defaultMargin: string;
  defaultCurrency: string;
  defaultValidityDays: number;
}

export interface QuotationDetail extends BaseEntity {
  itemType: QuotationItemType;
  referenceId: string;
  descriptionFrozen: string;
  quantity: string;
  basePriceHistorical: string;
  vatPercentHistorical: string;
  marginPercentHistorical: string;
  unitPriceFinal: string;
  lineSubtotalBase: string;
  lineVatValue: string;
  lineTotal: string;
}

export interface Quotation extends BaseEntity {
  quotationNumber: string;
  client: Client;
  createdByUser: User;
  issuedAt: string;
  validUntil: string;
  status: QuotationStatus;
  observations?: string | null;
  subtotal: string;
  discount: string;
  vatPercentHistorical: string;
  vatValueHistorical: string;
  total: string;
  currency: string;
  details: QuotationDetail[];
}

export interface Maintenance extends BaseEntity {
  client: Client;
  type: MaintenanceType;
  status: MaintenanceStatus;
  scheduledDate: string;
  executionDate?: string | null;
  technician: User;
  intervenedSystem: string;
  diagnosis: string;
  appliedSolution: string;
  observations?: string | null;
}

export interface MaintenanceComment extends BaseEntity {
  maintenance: Maintenance;
  user: User;
  comment: string;
}

export interface Attachment extends BaseEntity {
  sourceEntity: string;
  sourceEntityId: string;
  originalName: string;
  storedName: string;
  mimeType: string;
  storagePath: string;
  size: string;
  uploadedBy: string;
}

export interface AuditLog extends BaseEntity {
  module: string;
  entity: string;
  entityId: string;
  action: string;
  user: string;
  summary: string;
  payloadSummary?: string | null;
}
