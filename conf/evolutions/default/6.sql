# --- !Ups

update plan set name = substring(name from 9);

# --- !Downs

update plan set name = "Plan vom " || name;

