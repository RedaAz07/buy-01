import { Component, Input } from '@angular/core';
import { Product } from '../../core/models/post';
import { NgIf } from '@angular/common';
@Component({
  selector: 'app-post',
  imports: [NgIf],
  templateUrl: './post.html',
  styleUrl: './post.css',
})
export class Post {
  @Input() product!: Product
  fixPrice(p: number) {
    if (p >= 1000000) {
      return (p / 1000000).toFixed(2) + "M";
    } else if (p >= 10000) {
      return (p / 1000).toFixed(2) + "k";
    }
    return p.toFixed(2);
  }
}
