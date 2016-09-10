ALTER TABLE notes RENAME TO dog_ears;
ALTER TABLE dog_ears RENAME content TO note;
ALTER TABLE dog_ears ALTER note SET NOT NULL; 
