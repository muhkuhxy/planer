# --- !Ups

alter table schedule_services add column shift smallint;

# --- !Downs

alter table schedule_services drop column if exists shift;

