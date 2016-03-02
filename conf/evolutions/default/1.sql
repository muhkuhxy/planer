# --- !Ups

create table volunteer (
   id bigint(20) not null auto_increment,
   name varchar(255) not null,
   primary key (id)
);

create table service (
   id bigint(20) not null auto_increment,
   name varchar(255) not null,
   primary key (id)
);

create table volunteer_service (
   volunteer_id bigint(20) not null,
   service_id bigint(20) not null,
   foreign key(volunteer_id) references volunteer(id),
   foreign key(service_id) references service(id)
);

insert into service values
(1, 'sicherheit'),
(2, 'mikro'),
(3, 'tonanlage');

# --- !Downs

drop table volunteer;
drop table service;
drop table volunteer_service;
