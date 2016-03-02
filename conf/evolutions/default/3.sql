# --- !Ups

create index schedule_when_idx on schedule(when);
create unique index volunteer_name_index on volunteer(name);

# --- !Downs

drop index if exists schedule_when_idx;
drop index if exists volunteer_name_index;
