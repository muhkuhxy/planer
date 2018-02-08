# --- !Ups

alter table service add column slots smallint;
update service set slots = 1;
update service set slots = 2 where name in ('sicherheit', 'mikro');

# --- !Downs

alter table service drop column if exists slots;

