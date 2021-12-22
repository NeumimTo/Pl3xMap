CREATE TABLE %tablename% (
  world VARCHAR (32) NOT NULL,
  x INT,
  z INT,
  zoom INT,
  tile BLOB NOT NULL,
  INDEX %tablename%_idx (world,x,z)
)