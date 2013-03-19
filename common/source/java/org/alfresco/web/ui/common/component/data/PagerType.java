package org.alfresco.web.ui.common.component.data;

/**
 * Enumeration of the available Data Pager display types see ETWOONE-389 <br>
 * <br>
 * NB! this enmu was previously private enum in UIDataPager, but was moved out, to prevent NPE in development mode
 */
enum PagerType {
    STANDARD, DECADES, TRACKPAGE
}