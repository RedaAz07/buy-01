export interface UserProfileDTO {
  id: string,
  name: string,
  email: string,
  role: string,
  avatar: string | null,

}


export interface UpdateResponse {
  name: string,
  email: string,
  jwt: string
}

export interface UpdateRequest {
  name : string | null ,
  email: string | null,
}
