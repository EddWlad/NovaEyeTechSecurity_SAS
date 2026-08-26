import { Role } from './enums';

export type FieldType =
  | 'text'
  | 'email'
  | 'password'
  | 'textarea'
  | 'boolean'
  | 'number'
  | 'date'
  | 'select';

export interface SelectOption {
  label: string;
  value: string | boolean;
}

export interface ResourceField {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  requiredOnCreateOnly?: boolean;
  hideInList?: boolean;
  readonlyInEdit?: boolean;
  placeholder?: string;
  endpoint?: string;
  optionLabelKey?: string;
  optionValueKey?: string;
  options?: SelectOption[];
}

export interface ResourceDefinition {
  key: string;
  title: string;
  subtitle: string;
  endpoint: string;
  singular: string;
  roles: Role[];
  fields: ResourceField[];
}
