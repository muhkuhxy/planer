# --- !Ups

create table plan (
   id bigint(20) not null auto_increment,
   name varchar(255) not null,
   primary key (id)
);

create table schedule (
   id bigint(20) not null auto_increment,
   when date not null,
   plan_id bigint(20) not null,
   primary key (id),
   foreign key(plan_id) references plan(id)
);

create table unavailable (
   schedule_id bigint(20) not null,
   volunteer_id bigint(20) not null,
   foreign key(volunteer_id) references volunteer(id),
   foreign key(schedule_id) references schedule(id)
);

create table schedule_services (
   volunteer_id bigint(20) not null,
   service_id bigint(20) not null,
   schedule_id bigint(20) not null,
   foreign key(service_id) references service(id),
   foreign key(volunteer_id) references volunteer(id),
   foreign key(schedule_id) references schedule(id)
);

create table user (
   id bigint(20) not null auto_increment,
   username varchar(255) not null,
   password char(60) not null,
   primary key (id)
);

# --- !Downs

drop table plan;
drop table schedule;
drop table schedule_services;
drop table user;

