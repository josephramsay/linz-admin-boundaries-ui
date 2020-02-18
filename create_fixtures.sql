CREATE USER dab;
CREATE TABLE test (i INT, d VARCHAR);
ALTER TABLE test OWNER TO dab;
INSERT INTO test VALUES (100, 'abc');
          