import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Boutton } from '../../../components/boutton/boutton';
import { Auth } from '../../services/auth';


@Component({
  selector: 'app-navbar',
  imports: [RouterLink, Boutton],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})

export class Navbar {

  private userService = inject(Auth);
  private router = inject(Router)
  user = toSignal(this.userService.currentUser$, {
    initialValue: null
  });
  ngOnInit(){
    console.log("----------------------");

  }
  logout(): void {
    this.userService.logout();
  }
  route() {
    this.router.navigate(["/dashboard"])
  }
}
