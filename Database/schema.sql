
\c mini_dish_db

create type dish_types as enum ('START', 'MAIN', 'DESSERT');

create type categories as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table Dish
(
  id serial primary key,
  name varchar(250),
  dish_type dish_types
);

create table Ingredient
(
  id serial primary key,
  name varchar(250),
  price numeric(10, 2),
  category categories,
  id_dish int references Dish(id)
);
