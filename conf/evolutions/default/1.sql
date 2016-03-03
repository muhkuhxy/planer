# --- !Ups

create table volunteer (
   id bigserial,
   name varchar(255) not null,
   primary key (id)
);

create table service (
   id bigserial,
   name varchar(255) not null,
   primary key (id)
);

create table volunteer_service (
   volunteer_id bigint references volunteer(id),
   service_id bigint references service(id)
);

insert into service values
(1, 'sicherheit'),
(2, 'mikro'),
(3, 'tonanlage');

# --- !Downs

drop table volunteer;
drop table service;
drop table volunteer_service;
