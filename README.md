# Registry
## Synopsis
A utility program to maintain a registry in a key-value pair.

## Features
- Option to enable verbose mode i.e. to show information about each major step being carried out.
- Option to enable the non-interactive mode which will not show any confirmation prompt for any critical operation (e.g. while resetting or merging to database) and assumes every response as Yes by default.
- Option to reset the whole registry database.
- Option to merge a specified file (has to be in the correct format) to the existing registry database.
- Option to treat an external file (has to be in the correct format) as the registry database for the current session.
- Option to put the key and value data as entries to the existing database. If no key or value data is mentioned then the program shows the necessary prompts to receive the required data.
- Option to force entry of a value to an existing key in the registry database.
- Option to query a key from the existing registry database.

### Default behavior 
- Database file path is: {system specific temporary location}/registry/data/db 
- Verbose mode is disabled.
- Confirmation prompts for any critical operation (e.g. while resetting or merging to database) are shown.
- Best attempts will be made to ignore any database file error.
- Entry of value to an existing key will fail the insertion to the existing registry database.