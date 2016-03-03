# --- !Ups

create table plan (
   id bigserial,
   name varchar(255) not null,
   primary key (id)
);

create table schedule (
   id bigserial,
   day date not null,
   plan_id bigint references plan(id),
   primary key (id)
);

create table unavailable (
   schedule_id bigint references schedule(id),
   volunteer_id bigint references volunteer(id)
);

create table schedule_services (
   volunteer_id bigint references volunteer(id),
   service_id bigint references service(id),
   schedule_id bigint references schedule(id)
);

create table appuser (
   id bigserial,
   username varchar(255) not null,
   password char(60) not null,
   primary key (id)
);

# --- !Downs

drop table plan;
drop table schedule;
drop table schedule_services;
drop table appuser;

