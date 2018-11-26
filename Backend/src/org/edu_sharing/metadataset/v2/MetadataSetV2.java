package org.edu_sharing.metadataset.v2;

import java.util.*;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataWidget.Subwidget;
import org.edu_sharing.repository.client.tools.CCConstants;

public class MetadataSetV2 {
	Logger logger = Logger.getLogger(MetadataSetV2.class);

	public static String DEFAULT_CLIENT_QUERY="ngsearch";
	public static String DEFAULT_CLIENT_QUERY_CRITERIA = "ngsearchword";	
	
	private String id,repositoryId,label,i18n,name,inherit;
	private List<MetadataWidget> widgets;
	private boolean hidden;
	private List<MetadataTemplate> templates;
	private List<MetadataGroup> groups;
	private List<MetadataList> lists;
	private List<MetadataSort> sorts;
	private MetadataQueries queries;
	private MetadataCreate create;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}	
	
	public String getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInherit() {
		return inherit;
	}
	public void setInherit(String inherit) {
		this.inherit = inherit;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public List<MetadataWidget> getWidgets() {
		return widgets;
	}
	public List<MetadataTemplate> getTemplates() {
		return templates;
	}
	public List<MetadataGroup> getGroups() {
		return groups;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setWidgets(List<MetadataWidget> widgets) {
		this.widgets = widgets;
	}
	public void setTemplates(List<MetadataTemplate> templates) {
		this.templates = templates;		
	}
	public void setGroups(List<MetadataGroup> groups) {
		this.groups = groups;		
	}
	public String getI18n() {
		return i18n;
	}
	public void setI18n(String i18n) {
		this.i18n = i18n;
	}
	public MetadataQueries getQueries() {
		return queries;
	}
	public void setQueries(MetadataQueries queries) {
		this.queries = queries;
	}
	public MetadataCreate getCreate() {
		return create;
	}
	public void setCreate(MetadataCreate create) {
		this.create = create;
	}
	public List<MetadataList> getLists() {
		return lists;
	}
	public void setLists(List<MetadataList> lists) {
		this.lists = lists;
	}

	public List<MetadataSort> getSorts() {
		return sorts;
	}

	public void setSorts(List<MetadataSort> sorts) {
		this.sorts = sorts;
	}

	public void overrideWith(MetadataSetV2 mdsOverride) {
		if(mdsOverride.getId()!=null)
			setId(mdsOverride.getId());
		if(mdsOverride.getName()!=null)
			setName(mdsOverride.getName());
		for(MetadataWidget widget : mdsOverride.getWidgets()){
			if(!widget.isInherit()){
				List<MetadataWidget> widgetsRemove = findAllWidgets(widget.getId());
				widgets.removeAll(widgetsRemove);
			}
			if(widgets.contains(widget)){
				widgets.remove(widget);
				widgets.add(0,widget);
			}
			else{
				widgets.add(0,widget);
			}
		}
		for(MetadataTemplate template : mdsOverride.getTemplates()){
			if(templates.contains(template)){
				templates.remove(template);
				templates.add(0,template);
			}
			else{
				templates.add(0,template);
			}
		}
		for(MetadataGroup group : mdsOverride.getGroups()){
			if(groups.contains(group)){
				groups.remove(group);
				groups.add(0,group);
			}
			else{
				groups.add(0,group);
			}
		}
		for(MetadataList list : mdsOverride.getLists()){
			if(lists.contains(list)){
				lists.remove(list);
				lists.add(0,list);
			}
			else{
				lists.add(0,list);
			}
		}
		for(MetadataSort sort : mdsOverride.getSorts()){
			if(sorts.contains(sorts)){
				sorts.remove(sort);
				sorts.add(0,sort);
			}
			else{
				sorts.add(0,sort);
			}
		}
		if(mdsOverride.getCreate()!=null) {
			setCreate(mdsOverride.getCreate());
		}
		queries.overrideWith(mdsOverride.getQueries());
	}
	public MetadataWidget findWidget(String widgetId) {
		for(MetadataWidget widget : widgets){
			if(widget.getId().equals(widgetId))
				return widget;
		}
		throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
	}
	public List<MetadataWidget> findAllWidgets(String widgetId) {
		List<MetadataWidget> found = new ArrayList<>();
		for(MetadataWidget widget : widgets){
			if(widget.getId().equals(widgetId))
				found.add(widget);
		}
		if(found.size()>0)
			return found;
		throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
	}
	public MetadataGroup findGroup(String groupId) {
		for(MetadataGroup group : groups){
			if(group.getId().equals(groupId))
				return group;
		}
		throw new IllegalArgumentException("Group "+groupId+" was not found in the mds "+id);
	}
	public MetadataTemplate findTemplate(String templateId) {
		for(MetadataTemplate template : templates){
			if(template.getId().equals(templateId))
				return template;
		}
		throw new IllegalArgumentException("Template "+templateId+" was not found in the mds "+id);
	}
	public MetadataQuery findQuery(String queryId) {
		for(MetadataQuery query : queries.getQueries()){
			if(query.getId().equals(queryId))
				return query;
		}
		throw new IllegalArgumentException("Query "+queryId+" was not found in the mds "+id);
	}
	public List<MetadataWidget> getWidgetsByNode(String nodeType,Collection<String> aspects) {
		String group=null;
		if(CCConstants.CCM_TYPE_IO.equals(nodeType)) {
			group="io";
		}
		else if(CCConstants.CCM_TYPE_MAP.equals(nodeType)) {
			if(aspects.contains(CCConstants.CCM_ASPECT_COLLECTION)){
				group="collection_editorial";
			}
			else {
				group = "map";
			}
		}
		if(group==null) {
			logger.info("Node type "+nodeType+" currently not supported by backend, will use metadata from all available widgets");
			return getWidgets();
		}
		return getWidgetsByTemplate(group);
	}

	public List<MetadataWidget> getWidgetsByTemplate(String template) {
		List<MetadataWidget> usedWidgets=new ArrayList<>();
		for(String view : findGroup(template).getViews()) {
			String html = findTemplate(view).getHtml();
			for(MetadataWidget widget : getWidgets()) {
				if(html.indexOf("<"+widget.getId())>-1) {
					usedWidgets.add(widget);
					// handle group (sub) widgets
					if(widget.getSubwidgets()!=null && widget.getSubwidgets().size()>0) {
						for(Subwidget subwidget : widget.getSubwidgets()) {
							usedWidgets.addAll(findAllWidgets(subwidget.getId()));
						}
					}
				}
			}

		}
		logger.info("Template "+template+" uses "+usedWidgets.size()+" from a total of "+getWidgets().size()+" widgets");
		return usedWidgets;
	}

	public MetadataWidget findWidgetForTemplateAndCondition(String widgetId, String template, Map<String, String[]> properties) {
		  List<MetadataWidget> found=new ArrayList<>();
		  boolean hasTemplate=false;
		  for(MetadataWidget widget : widgets){
		   if(widget.getId().equals(widgetId))
			   found.add(widget);
		   if(widget.getId().equals(widgetId) && template.equals(widget.getTemplate())) {
		   		if(!hasTemplate) found.clear();
		   		hasTemplate=true;
			   found.add(widget);
		   }
		  }
		  if(found.size()==0)
		  	throw new IllegalArgumentException("Widget "+widgetId+" was not found in the mds "+id);
		  for(MetadataWidget widget : found) {
			  boolean allowed = true;
			  MetadataWidget.Condition cond = widget.getCondition();
			  if (cond != null && cond.getType().equals(MetadataWidget.Condition.CONDITION_TYPE.PROPERTY)) {
			  	  String[] value=properties.get(CCConstants.getValidGlobalName(cond.getValue()));
				  boolean empty = isValueEmpty(value);

			  	  allowed=empty==cond.isNegate();
			  }
			  if(allowed) {
				  return widget;
			  }
		  }
		  found.get(0).setHideIfEmpty(true);
		  return found.get(0);
	 }

	private boolean isValueEmpty(String[] value) {
		boolean empty=value==null || value.length==0;
		if(!empty){
			empty=true;
			for(String check :value){
				if(check!=null && !check.isEmpty())
					empty=false;
		}
	  }
		return empty;
	}
}
