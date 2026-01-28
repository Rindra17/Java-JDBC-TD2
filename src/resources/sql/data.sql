insert into dish (id, name, dish_type, selling_price)
values (1, 'Salaide fraîche', 'STARTER', 3500.0),
       (2, 'Poulet grillé', 'MAIN', 12000.0),
       (3, 'Riz aux légumes', 'MAIN', null),
       (4, 'Gâteau au chocolat ', 'DESSERT', 8000.0),
       (5, 'Salade de fruits', 'DESSERT', null);


insert into ingredient (id, name, category, price)
values (1, 'Laitue', 'VEGETABLE', 800.0),
       (2, 'Tomate', 'VEGETABLE', 600.0),
       (3, 'Poulet', 'ANIMAL', 4500.0),
       (4, 'Chocolat ', 'OTHER', 3000.0),
       (5, 'Beurre', 'DAIRY', 2500.0);



update dish
set price = 2000.0
where id = 1;

update dish
set price = 6000.0
where id = 2;


insert into stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime)
values (1, 1, 5.0, 'IN', 'KG', '2024-01-05 08:00'),
       (2, 1, 0.2, 'OUT', 'KG', '2024-01-06 12:00'),
       (3, 2, 4.0, 'IN', 'KG', '2024-01-05 08:00'),
       (4, 2, 0.15, 'OUT', 'KG', '2024-01-06 12:00'),
       (5, 3, 10.0, 'IN', 'KG', '2024-01-04 09:00'),
       (6, 3, 1.0, 'OUT', 'KG', '2024-01-06 13:00'),
       (7, 4, 3.0, 'IN', 'KG', '2024-01-05 10:00'),
       (8, 4, 0.3, 'OUT', 'KG', '2024-01-06 14:00'),
       (9, 5, 2.5, 'IN', 'KG', '2024-01-05 10:00'),
       (10, 5, 0.2, 'OUT', 'KG', '2024-01-06 14:00');


insert into dish_ingredient (id, id_dish, id_ingredient, required_quantity, unit)
values (1, 1, 1, 0.2, 'KG'),
       (2, 1, 2, 0.15, 'KG'),
       (3, 2, 3, 1.0, 'KG'),
       (4, 4, 4, 0.3, 'KG'),
       (5, 4, 5, 0.2, 'KG');