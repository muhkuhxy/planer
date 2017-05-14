# --- !Ups

alter table volunteer add column active boolean default true;
insert into volunteer (name, active) values ('geloescht', false);

# --- !Downs

alter table volunteer drop column if exists active;
delete from volunteer where name = 'geloescht';

