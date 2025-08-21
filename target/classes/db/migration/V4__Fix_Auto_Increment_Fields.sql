-- Fix AUTO_INCREMENT for primary key fields
-- Handle ALL foreign key constraints properly

-- Step 1: Drop ALL foreign key constraints that reference que_id
ALTER TABLE exam_option DROP FOREIGN KEY FK5v5uvc47r0e02i4i329c0timh;
ALTER TABLE exam_option DROP FOREIGN KEY FK8klmro2xx63jstv0k2lhe42j3;
ALTER TABLE exam_option DROP FOREIGN KEY FKky5sj7tsnlu9xie9xyctavd9x;
ALTER TABLE question_correct_options DROP FOREIGN KEY FKqw7r17qu3j9ro4yqraj19blag;

-- Step 2: Drop foreign key constraints that reference exam_id
ALTER TABLE question DROP FOREIGN KEY FKhupso6ldavcx993tfnrjsdl1p;

-- Step 2: Modify the question table to add AUTO_INCREMENT
ALTER TABLE question MODIFY COLUMN que_id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 3: Fix the exam table
ALTER TABLE exam MODIFY COLUMN exam_id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 4: Fix the exam_option table option_id
ALTER TABLE exam_option MODIFY COLUMN option_id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 5: Recreate ALL the foreign key constraints
ALTER TABLE exam_option ADD CONSTRAINT FK5v5uvc47r0e02i4i329c0timh 
    FOREIGN KEY (qid) REFERENCES question(que_id);

ALTER TABLE exam_option ADD CONSTRAINT FK8klmro2xx63jstv0k2lhe42j3 
    FOREIGN KEY (q_id) REFERENCES question(que_id);

ALTER TABLE exam_option ADD CONSTRAINT FKky5sj7tsnlu9xie9xyctavd9x 
    FOREIGN KEY (q_id) REFERENCES question(que_id);

ALTER TABLE question_correct_options ADD CONSTRAINT FKqw7r17qu3j9ro4yqraj19blag 
    FOREIGN KEY (question_que_id) REFERENCES question(que_id);

-- Step 6: Recreate the foreign key constraint for exam_id  
ALTER TABLE question ADD CONSTRAINT FKhupso6ldavcx993tfnrjsdl1p 
    FOREIGN KEY (ex_id) REFERENCES exam(exam_id);