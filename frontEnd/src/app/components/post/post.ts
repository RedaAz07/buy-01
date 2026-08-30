import { Component, Input } from '@angular/core';
import { Productdto } from '../../core/models/post';
import { NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
@Component({
  selector: 'app-post',
  imports: [NgIf,RouterLink],
  templateUrl: './post.html',
  styleUrl: './post.css',
})
export class Post {
  @Input() product!: Productdto

  lotNo(): string {
    const id = String(this.product.id).toLowerCase().replace(/[^0-9a-f]/g, '');
    if (!id) {
      return '000';
    }
    const n = parseInt(id.slice(-6), 16);
    return String(isNaN(n) ? 1 : n % 1000).padStart(3, '0');
  }

  fixPrice(p: number) {
    if (p >= 1000000) {
      return (p / 1000000).toFixed(2) + "M";
    } else if (p >= 10000) {
      return (p / 1000).toFixed(2) + "k";
    }
    return p.toFixed(2);
  }
  gotoproduct(){
    
  }
}
