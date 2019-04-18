# --- !Ups

create index vs_volunteer_fk on volunteer_service (volunteer_id);
create index vs_service_fk on volunteer_service (service_id);

# --- !Downs

drop index if exists vs_volunteer_fk;
drop index if exists vs_service_fk;
