import { Component, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Boutton } from '../../../components/boutton/boutton';
import { Auth } from '../../services/auth';
import { Subject, takeUntil } from 'rxjs';


@Component({
  selector: 'app-navbar',
  imports: [RouterLink, Boutton],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})

export class Navbar implements OnInit {

  private userService = inject(Auth);
  private router = inject(Router)
  // user = toSignal(this.userService.currentUser$, {
  //   initialValue: null
  // });
  user = signal<any>(null)

  ngOnInit(): void {
    this.userService.currentUser$
      .pipe()
      .subscribe((user) => {
        console.log(user);
        
        this.user.set(user);
      });
  }
  logout(): void {
    this.userService.logout();
  }
  route() {
    this.router.navigate(["/dashboard"])
  }
}
