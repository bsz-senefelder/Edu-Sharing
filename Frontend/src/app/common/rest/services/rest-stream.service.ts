import { Injectable } from '@angular/core';
import { Http, Response } from '@angular/http';
import 'rxjs/add/operator/map'
import { Observable } from 'rxjs/Observable';
import {RestConnectorService} from "./rest-connector.service";
import {RestHelper} from "../rest-helper";
import {RestConstants} from "../rest-constants";
import {
  ArchiveRestore, ArchiveSearch, Node, MdsMetadatasets, MdsMetadataset, MdsValues,
  MdsValueList, STREAM_STATUS
} from "../data-object";

@Injectable()
export class RestStreamService {
  constructor(private connector : RestConnectorService) {}

  public getStream = (status:STREAM_STATUS = STREAM_STATUS.OPEN,queryString:string="",queryProperties:any={},request:any=null,repository=RestConstants.HOME_REPOSITORY): Observable<MdsMetadatasets> => {
    let query=this.connector.createUrlNoEscape("stream/:version/search/:repository?status=:status&query=:query&:request",repository,[
      [":status",encodeURIComponent(status)],
      [":query",encodeURIComponent(queryString)],
      [":request",this.connector.createRequestString(request)]
    ]);
    return this.connector.post(query,JSON.stringify(queryProperties),this.connector.getRequestOptions())
      .map((response: Response) => response.json());

  }
  public addEntry = (entry:any,repository=RestConstants.HOME_REPOSITORY): Observable<any> => {
    let query=this.connector.createUrl("stream/:version/add/:repository",repository);
    return this.connector.put(query,JSON.stringify(entry),this.connector.getRequestOptions())
      .map((response: Response) => response.json());
  }
  public updateStatus = (entry:string,authority:string,status:STREAM_STATUS,repository=RestConstants.HOME_REPOSITORY): Observable<Response> => {
    let query=this.connector.createUrl("stream/:version/status/:repository/:entry?authority=:authority&status=:status",repository,[
      [":entry",entry],
      [":authority",authority],
      [":status",status],
    ]);
    return this.connector.put(query,null,this.connector.getRequestOptions());
  }
}
