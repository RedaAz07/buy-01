import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { Boutton } from '../boutton/boutton';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink,Boutton],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})

export class Navbar {

  private userService = inject(Auth);

  user = toSignal(this.userService.currentUser$, {
    initialValue: null
  });

  logout(): void {
    this.userService.logout();
  }

}
