
CREATE DATABASE mini_dish_db ;

CREATE ROLE mini_dish_db_manager WITH LOGIN PASSWORD 'your_password';

\c mini_dish_db

grant create on schema public to mini_dish_db_manager;

alter default privileges in schema public
  grant select, insert, update, delete on tables to mini_dish_db_manager;

alter default privileges in schema public
  grant usage, select, update on sequences to mini_dish_db_manager;