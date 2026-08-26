export interface LoginRequestDTO {
  name: string;
  password: string;
}
export interface RegisterRequestDTO {
  name: string;
  email: string;
  password: string;
  role: string;
}
export interface AuthResponseDTO {
  jwt: string;
}
