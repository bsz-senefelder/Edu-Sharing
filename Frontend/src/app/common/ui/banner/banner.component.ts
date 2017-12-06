/**
 * Created by Torsten on 13.01.2017.
 */

import {Component, Input} from '@angular/core';
import {ConfigurationService} from "../../services/configuration.service";
import {ConfigurationHelper} from "../../rest/configuration-helper";

@Component({
  selector: 'app-banner',
  templateUrl: 'banner.component.html',
  styleUrls: ['banner.component.scss'],
})
export class BannerComponent {
  @Input() scope:string;
  public banner: any;
  constructor(private config:ConfigurationService) {
    this.config.getAll().subscribe(()=>{
      this.banner = ConfigurationHelper.getBanner(this.config);
    });
  }
}
