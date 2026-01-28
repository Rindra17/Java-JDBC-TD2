create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT');


create table dish
(
    id        serial primary key,
    name      varchar(255),
    dish_type dish_type
);

create type ingredient_category as enum ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

create table ingredient
(
    id       serial primary key,
    name     varchar(255),
    price    numeric(10, 2),
    category ingredient_category
);

alter table dish
    add column if not exists price numeric(10, 2);

alter table dish
    rename column price to selling_price;

alter table ingredient
    drop column if exists id_dish;

alter table ingredient
    add column if not exists required_quantity numeric(10, 2);

alter table ingredient
    drop column if exists required_quantity;

create type unit as enum ('PCS', 'KG', 'L');

create table if not exists dish_ingredient
(
    id                serial primary key,
    id_ingredient     int,
    id_dish           int,
    required_quantity numeric(10, 2),
    unit              unit,
    foreign key (id_ingredient) references ingredient (id),
    foreign key (id_dish) references dish (id)
);

create type movement_type as enum ('IN', 'OUT');

create table if not exists stock_movement
(
    id                serial primary key,
    id_ingredient     int,
    quantity          numeric(10, 2),
    unit              unit,
    type              movement_type,
    creation_datetime timestamp without time zone,
    foreign key (id_ingredient) references ingredient (id)
);


alter table ingredient
    add column if not exists initial_stock numeric(10, 2);

create table if not exists "order"
(
    id                serial primary key,
    reference         varchar(255),
    creation_datetime timestamp without time zone
);

create table if not exists dish_order
(
    id       serial primary key,
    id_order int references "order" (id),
    id_dish  int references dish (id),
    quantity int
);

alter table "order"
    add constraint  order_reference_unique unique (reference);