import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { HttpModule } from '@angular/http';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import * as EduData from "../../common/rest/data-object"; //
import {RestCollectionService} from "../../common/rest/services/rest-collection.service"; //
import {Toast} from "../../common/ui/toast"; //
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestMetadataService} from '../../common/rest/services/rest-metadata.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Node, NodeList, LoginResult, STREAM_STATUS, ConnectorList} from '../../common/rest/data-object';
import {OptionItem} from "../../common/ui/actionbar/option-item";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestMdsService} from "../../common/rest/services/rest-mds.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {ListItem} from "../../common/ui/list-item";
import {MdsHelper} from "../../common/rest/mds-helper";
import {NodeHelper} from "../../common/ui/node-helper"; //
import {ActionbarHelper} from "../../common/ui/actionbar/actionbar-helper"; //
import {Observable} from 'rxjs/Rx';
import {RestStreamService} from "../../common/rest/services/rest-stream.service";
import {RestConnectorsService} from '../../common/rest/services/rest-connectors.service';
import {UIAnimation} from '../../common/ui/ui-animation';
import {trigger} from '@angular/animations';
import {Connector} from '../../common/rest/data-object';
import {NodeWrapper} from '../../common/rest/data-object';
import {Filetype} from '../../common/rest/data-object';
import {FrameEventsService} from '../../common/services/frame-events.service';
import {CordovaService} from '../../common/services/cordova.service';
import 'rxjs/add/operator/pairwise';
import { Subscription } from 'rxjs/Subscription';
import { RoutesRecognized } from '@angular/router';


@Component({
  selector: 'app-stream',
  templateUrl: 'stream.component.html',
  styleUrls: ['stream.component.scss'],
  animations:[
      trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
  ]
  })


export class StreamComponent {
  today() {
      var d = new Date();
      var weekday = d.getDay();
      var dd = d.getDate(); 
      var mm = d.getMonth()+1; //January is 0!
      var yyyy = String(d.getFullYear());
      var outstring = '';
      if (weekday == 0) outstring += 'Sonntag, der ';
      if (weekday == 1) outstring += 'Montag, der ';
      if (weekday == 2) outstring += 'Dienstag, der ';
      if (weekday == 3) outstring += 'Mittwoch, der ';
      if (weekday == 4) outstring += 'Donnerstag, der ';
      if (weekday == 5) outstring += 'Freitag, der ';
      if (weekday == 6) outstring += 'Samstag, der ';
      if(dd<10) {outstring += '0'+String(dd);} else {outstring += String(dd);}
      outstring += '. ';
      if(mm<10) {outstring += '0'+String(mm);} else {outstring +=  String(mm);}
      return outstring + '. ' + String(yyyy);
  }
  connectorList: ConnectorList;
  createConnectorName: string;
  createConnectorType: Connector;
  createAllowed : boolean ;
  showCreate = false;
  public collectionNodes:EduData.Node[];
  public tabSelected:string = RestConstants.COLLECTIONSCOPE_MY;
  public mainnav = true;
  public nodeReport: Node;
  public globalProgress=false;
  menuOption = 'new';
  showMenuOptions = false;
  streams: any;
  actionOptions:OptionItem[]=[];
  pageOffset: number;
  imagesToLoad = 0;
  imagesLoaded = 0;
  shouldOpen = false;
  routerSubscription: Subscription;

  moveUpOption = new OptionItem('STREAM.OBJECT.OPTION.MOVEUP','check',(node: Node)=>{
    this.updateStream(node, STREAM_STATUS.PROGRESS).subscribe( (data) => {
      this.updateDataFromJSON(STREAM_STATUS.OPEN);
      this.toast.toast("STREAM.TOAST.MOVEUP");
    }, error => console.log(error));
  });

  collectionOption = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers",(node: Node) => this.addToCollection(node));

  removeOption = new OptionItem('STREAM.OBJECT.OPTION.REMOVE','remove_circle',(node: Node)=> {
    this.updateStream(node, STREAM_STATUS.DONE).subscribe( (data) => {
      let result = this.streams.filter( (n : any) => n.id !== node );
      this.streams = result;
    } , error => console.log(error));
  });

  // TODO: Store and use current search query
  searchQuery:string;
  doSearch(query:string){
    this.searchQuery=query;
    console.log(query);
    // TODO: Search for the given query doch nicht erledigt
  }
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private connectors:RestConnectorsService,
    private nodeService: RestNodeService,
    private cordova: CordovaService,
    private searchService: RestSearchService,
    private metadataService:RestMetadataService,
    private event:FrameEventsService,
    private streamService:RestStreamService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private toast : Toast,
    private collectionService : RestCollectionService,
    private config : ConfigurationService,
    private http: Http,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=>{
        UIHelper.setTitle('STREAM.TITLE',title,translate,config);
        this.connector.isLoggedIn().subscribe(data => {
            console.log(data);
            this.createAllowed=data.statusCode==RestConstants.STATUS_CODE_OK;
        });
          this.connectors.list().subscribe(list=>{
              this.connectorList=list;
          });
      });
      this.setStreamMode();
      this.routerSubscription = this.router.events
      .filter(e => e instanceof RoutesRecognized)
      .pairwise()
      .subscribe((e: any[]) => {
        document.cookie = "scroll="+"noScroll";
        if (/components\/render/.test(e[0].urlAfterRedirects)) {
          this.route.queryParams.subscribe((params: Params) => {
            console.log("params.mode", params.mode);
            if (params.mode === 'seen') {
              document.cookie = "scroll="+"seen";
            }
            if (params.mode === 'new') {
              document.cookie = "scroll="+"new";
              this.toast.toast('STREAM.TOAST.SEEN');
            }
          });
          this.routerSubscription.unsubscribe();
        }
      });
  }

  setStreamMode() {
    this.route.queryParams.subscribe((params: Params) => {
      if (params.mode === 'new') {
        this.menuOptions(params.mode);
      }
      if (params.mode === 'seen') {
        this.menuOptions(params.mode);
      }
      else {
        this.menuOptions('new');
      }
    });
  }

  seen(id: any) {
    this.updateStream(id, STREAM_STATUS.READ).subscribe(data => this.updateDataFromJSON(STREAM_STATUS.OPEN) , error => console.log(error));
  }

  onScroll() {
    console.log("scrolled!!");
    //this.getJSON().subscribe(data => this.streams = this.streams.concat(data['stream']), error => console.log(error));
  }

  toggleMenuOptions() {
    this.showMenuOptions = !this.showMenuOptions;
    if (this.showMenuOptions) {
      this.shouldOpen = true;
    }
  }

  closeMenuOptions() {
    this.showMenuOptions = false;
    if (this.shouldOpen) {
      this.showMenuOptions = true;
      this.shouldOpen = false;
    }
  }

  checkIfEnable(nodes: any) {
    this.collectionOption.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
  }

  finishedLoad() {
    this.imagesLoaded++;
    console.log("to load: ", this.imagesToLoad);
    console.log("images loaded", this.imagesLoaded);
    if (this.imagesLoaded === this.imagesToLoad) {
      this.scrollToDown();
    }
  }

  scrollToDown() {
    console.log(this.getCookie("jumpToScrollPosition"));
    let pos = Number(this.getCookie("jumpToScrollPosition"));
    let whichScroll = this.getCookie("scroll");
    console.log("which: ", whichScroll);
    if (whichScroll !== "noScroll"){
      console.log("scroll to: ",pos);
      window.scrollTo(0,pos);
    }
    document.cookie = "scroll="+"noScroll";
  }

  getCookie(cname: any) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for(let i = 0; i <ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

  menuOptions(option: any) {
    this.menuOption = option;
    if (option === 'new') {
      this.router.navigate(["./"],{queryParams:{mode:this.menuOption},relativeTo:this.route})
      this.streams = [];
      this.updateDataFromJSON(STREAM_STATUS.OPEN);
      this.actionOptions[0] = this.moveUpOption;
      this.actionOptions[1] = this.collectionOption;
    } else {
      this.router.navigate(["./"],{queryParams:{mode:this.menuOption},relativeTo:this.route})
      this.streams = [];
      this.updateDataFromJSON(STREAM_STATUS.READ);
      this.actionOptions[0] = this.collectionOption;
      this.actionOptions[1] = this.removeOption;
    }

  }

  updateDataFromJSON(streamStatus: any) {
    this.imagesLoaded = 0;
    if (streamStatus == STREAM_STATUS.OPEN) {
      let openStreams: any[];
      let progressStreams: any[];
      this.getSimpleJSON(STREAM_STATUS.OPEN).subscribe(data => {
        openStreams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
        this.getSimpleJSON(STREAM_STATUS.PROGRESS).subscribe(data => {
          progressStreams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
          this.streams = progressStreams.concat(openStreams);
          this.imagesToLoad = this.streams.length;
        });
      }, error => console.log(error));
    }
    else {
      this.getSimpleJSON(streamStatus).subscribe(data => {
        this.streams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
        this.imagesToLoad = this.streams.length;
      }, error => console.log(error));
    }

  }

  refresh(): void {
    window.location.reload();
  }

  onStreamObjectClick(node: any) {
    console.log(node.nodes[0].ref.id);
    this.seen(node.id);
    document.cookie = "jumpToScrollPosition="+window.pageYOffset;
    this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.nodes[0].ref.id])
  }

  private addToCollection(node: EduData.Node) {
    let result = this.streams.filter( (n: any) => (n.id == node) ).map( (n: any) => { return n.nodes } );
    this.collectionNodes = [].concat.apply([], result);

  }

  public getJSON(streamStatus: any): Observable<any> {
    let request:any={offset:this.streams ? this.streams.length : 0};
    return this.streamService.getStream(streamStatus,this.searchQuery,{},request);
  }

  public getSimpleJSON(streamStatus: any): Observable<any> {
    let request:any={offset: 0, sortProperties:["priority","created"],sortAscending:[false,false]};
    return this.streamService.getStream(streamStatus,this.searchQuery,{},request);
  }

  public updateStream(idToUpdate: any, status: any): Observable<any> {
    return this.streamService.updateStatus(idToUpdate, this.connector.getCurrentLogin().authorityName, status)
  }
    private create(){
        if(!this.createAllowed)
            return;
        this.showCreate = true;
    }
    private createConnector(event : any){
        this.createConnectorName=null;
        let prop=NodeHelper.propertiesFromConnector(event);
        let win:any;
        if(!this.cordova.isRunningCordova())
            win=window.open("");
        this.nodeService.createNode(RestConstants.INBOX,RestConstants.CCM_TYPE_IO,[],prop,false).subscribe(
            (data : NodeWrapper)=>{
                this.editConnector(data.node,event.type,win,this.createConnectorType);
                UIHelper.goToWorkspaceFolder(this.nodeService,this.router,null,RestConstants.INBOX);
            },
            (error : any)=>{
                win.close();
                if(NodeHelper.handleNodeError(this.toast,event.name,error)==RestConstants.DUPLICATE_NODE_RESPONSE){
                    this.createConnectorName=event.name;
                }
            }
        )

    }
    private editConnector(node:Node,type : Filetype=null,win : any = null,connectorType : Connector = null){
        UIHelper.openConnector(this.connectors,this.event,this.toast,this.connectorList,node,type,win,connectorType);
    }
}
