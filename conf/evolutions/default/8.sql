# --- !Ups

delete from volunteer_service where volunteer_id = (select id from volunteer where name = 'geloescht');
delete from schedule_services where volunteer_id = (select id from volunteer where name = 'geloescht');
delete from unavailable where volunteer_id = (select id from volunteer where name = 'geloescht');
delete from volunteer where name = 'geloescht';

alter table volunteer drop column if exists active;

# --- !Downs

alter table volunteer add column active boolean default true;

insert into volunteer (name, active) values ('geloescht', false);

