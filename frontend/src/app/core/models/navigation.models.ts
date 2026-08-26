import { Role } from './enums';

export interface NavigationItem {
  label: string;
  icon: string;
  route: string;
  roles: Role[];
}
