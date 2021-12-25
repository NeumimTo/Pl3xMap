CREATE TABLE %tablename% (
  world VARCHAR (32) NOT NULL,
  x INT,
  z INT,
  zoom INT,
  tile BLOB NOT NULL,
  primary key (world,x,z,zoom)
)