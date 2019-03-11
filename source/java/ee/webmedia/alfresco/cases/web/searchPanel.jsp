<a:panel id="case-search-panel" styleClass="with-pager" label="Otsing" rendered="#{DialogManager.bean.parent.containsCases}">
   <a:panel id="search-panel">
      <h:inputText id="search-text" styleClass="focus" value="#{DialogManager.bean.searchCriteria}" size="35" maxlength="1024" />
      <h:commandButton id="search-btn" value="#{msg.search}" action="#{DialogManager.bean.search}" disabled="false" style="margin-left: 5px;" styleClass="specificAction" />
      <h:commandButton id="show-all-button" value="#{msg.show_all}" action="#{DialogManager.bean.showAll}" style="margin-left: 5px;" />
   </a:panel>
</a:panel>