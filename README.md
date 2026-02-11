# CSV Parser (Streaming → MySQL)

A production-style CLI tool that **streams CSV files**, validates them against a **JSON schema**, and writes results into MySQL:
- valid rows → target data table
- invalid rows (CSV errors / validation errors / DB insert failures) → `error_rows`

Designed with clean boundaries (ports/adapters), SOLID-friendly structure, and easy review.

---

## Features
- Streaming CSV read (does not load full files into memory)
- Multiple inputs (file/folder), deterministic file ordering
- Optional recursive scan (if enabled in CLI)
- Optional type validation (`--validate-types`)
- Optional column selection (`--include-columns`)
- Batch inserts (`--batch-size`) for performance
- Resilient batch failure handling (isolates bad rows and continues)
- Centralized error capture into `error_rows` for:
  - file-level errors (e.g., header mismatch)
  - row-level parse errors
  - validation/type errors
  - DB insert errors (isolated rows)

---

## How it works (end-to-end flow)
1) **Load schema**
   - `JsonSchemaLoader` reads schema JSON
   - `SchemaValidator` validates schema structure/constraints

2) **Resolve inputs**
   - `InputResolver` converts input paths into a deterministic list of `.csv` files
   - supports folder scanning (and optional recursion)

3) **Prepare database**
   - `TableWriter` ensures the target data table exists (schema-driven)
   - `ErrorWriter` ensures `error_rows` exists

4) **Stream parse**
   - `CsvRowSource` opens a `CsvRowCursor` per file
   - reads rows sequentially (streaming)

5) **Validate / transform**
   - `RowTransformer` chosen based on `--validate-types`:
     - `NoOpRowTransformer` when disabled (just selects columns)
     - `TypedRowTransformer` when enabled (converts/validates types)
   - rejected rows are written to `error_rows`

6) **Batch insert**
   - valid rows are collected into a batch list
   - inserted via `MySqlBatchInserter`
   - on failure: uses savepoints + divide-and-conquer isolation to find and record bad rows, while continuing with the rest

---

## CLI (Picocli) design
This project uses **Picocli** for a clean CLI interface:
- A top-level entry command registers subcommands (e.g., `parse`)
- The `parse` subcommand maps CLI flags into a request object (e.g., `ParseRequest`)
- `ParseCsvUseCase.execute(request)` runs the import
- Picocli provides:
  - `--help` usage output
  - required option validation
  - type conversion for flags (`Path`, `int`, `boolean`, etc.)
  - default values (e.g., batch size)



---

## Run the app
> Replace `<MAIN_CLASS>` with the actual entrypoint class you run in your project.

```bash
java -cp target/csv-parser-1.0.0.jar <MAIN_CLASS> parse \
  --schema ./tmp/schema.json \
  --input  ./tmp/data.csv \
  --db-url "jdbc:mysql://localhost:3306/csvdb" \
  --db-user "csv" \
  --db-pass "csv" \
  --batch-size 1000 \
  --validate-types
