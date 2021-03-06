drop table alf_acl_member;
drop table alf_access_control_entry;
alter table alf_node drop constraint fk_alf_node_acl;
alter table alf_attributes drop constraint fk_alf_attr_acl;
alter table avm_nodes drop constraint fk_avm_n_acl;
alter table avm_stores drop constraint fk_avm_s_acl;
drop table alf_access_control_list;
drop table alf_ace_context;
drop table alf_acl_change_set;
drop table alf_permission;