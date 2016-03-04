# --- !Ups

alter table schedule_services add column shift smallint;
alter table volunteer add column email varchar(255);

# --- !Downs

alter table schedule_services drop column if exists shift;
alter table volunteer drop column if exists email;

