SELECT id, source_file, row_num, raw_row, error_message
FROM error_rows
ORDER BY id DESC
LIMIT 5;