# Forum API Documentation

## Authorization
All endpoints require JWT authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

## Question Endpoints
### Get Single Question
```http
GET /forum/questions/{id}
```
Retrieves a specific question by ID.

**Parameters:**
- `id` (path): Question ID

**Responses:**
- 200: Question details
```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "files": ["string"],
  "userId": {"$oid": "string"},
  "courseId": {"$oid": "string"},
  "timestamp": "long",
  "answers": [{"$oid": "string"}],
  "section": "string",
  "lecture": "string",
  "views": "integer",
  "votes": "integer"
}
```
- 400: Bad Request - Question ID required
- 404: Question not found


### Post Question
```http
POST /courses/{courseId}/forum
```
Creates a new question in a course forum.

**Parameters:**
- `courseId` (path): ID of the course

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "files": ["string"],
  "section": "string",
  "lecture": "string"
}
```

**Responses:**
- 200: Question added successfully
- 400: Bad Request - Missing courseId
- 401: Unauthorized
- 403: Not enrolled in course
- 404: Course not found

### Get Questions
```http
GET /courses/{courseId}/forum
```
Retrieves all questions for a course.

**Parameters:**
- `courseId` (path): ID of the course

**Responses:**
- 200: List of questions
- 400: Bad Request - Course ID required
- 401: Unauthorized

### ### Delete Question
```http
DELETE /forum/questions/{id}
```
Deletes a specific question. Only the author can delete their question.
The operation will:
1. Remove the question reference from the course
2. Delete the question document

**Implementation Notes:**
- Verifies question exists before attempting deletion
- Checks user authorization
- Handles MongoDB ObjectId conversion errors
- Removes question reference from course document

**Parameters:**
- `id` (path): Question ID

**Responses:**
- 200: Question deleted successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized - User is not the author
- 404: Question not found

### Edit Question
```http
PUT /forum/questions/{id}
```
Updates a specific question. Only the author can edit their question.

**Implementation Notes:**
- Verifies question exists before attempting update
- Checks user authorization
- Handles MongoDB ObjectId conversion errors
- Maintains existing question metadata while updating editable fields

**Parameters:**
- `id` (path): Question ID

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "files": ["string"],
  "section": "string",
  "lecture": "string"
}
```

**Responses:**
- 200: Question updated successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized - User is not the author
- 404: Question not found

## Answer Endpoints

### Post Answer
```http
POST /forum/questions/{questionId}/answers
```
Adds an answer to a question.

**Parameters:**
- `questionId` (path): Question ID

**Request Body:**
```json
{
  "userRole": "string",
  "description": "string",
  "files": ["string"]
}
```

**Responses:**
- 200: Answer added successfully
- 400: Bad Request - Question ID required
- 401: Unauthorized
- 404: Question not found

### Get Answers
```http
GET /forum/questions/{questionId}/answers
```
Retrieves all answers for a question.

**Parameters:**
- `questionId` (path): Question ID

**Responses:**
- 200: List of answers
- 400: Bad Request - Question ID required
- 401: Unauthorized

### Delete Answer
```http
DELETE /forum/answers/{id}
```
Deletes a specific answer. Only the author can delete their answer.

**Parameters:**
- `id` (path): Answer ID

**Responses:**
- 200: Answer deleted successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized
- 404: Answer not found

### Edit Answer
```http
PUT /forum/answers/{id}
```
Updates a specific answer. Only the author can edit their answer.

**Parameters:**
- `id` (path): Answer ID

**Request Body:**
```json
{
  "userRole": "string",
  "description": "string",
  "files": ["string"]
}
```

**Responses:**
- 200: Answer updated successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized
- 404: Answer not found

## Reply Endpoints

### Post Reply
```http
POST /forum/answers/{answerId}/replies
```
Adds a reply to an answer.

**Parameters:**
- `answerId` (path): Answer ID

**Request Body:**
```json
{
  "userRole": "string",
  "description": "string",
  "files": ["string"]
}
```

**Responses:**
- 200: Reply added successfully
- 400: Bad Request - Answer ID required
- 401: Unauthorized
- 404: Answer not found

### Get Replies
```http
GET /forum/answers/{answerId}/replies
```
Retrieves all replies for an answer.

**Parameters:**
- `answerId` (path): Answer ID

**Responses:**
- 200: List of replies
- 400: Bad Request - Answer ID required
- 401: Unauthorized

### Delete Reply
```http
DELETE /forum/replies/{id}
```
Deletes a specific reply. Only the author can delete their reply.

**Parameters:**
- `id` (path): Reply ID

**Responses:**
- 200: Reply deleted successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized
- 404: Reply not found

### Edit Reply
```http
PUT /forum/replies/{id}
```
Updates a specific reply. Only the author can edit their reply.

**Parameters:**
- `id` (path): Reply ID

**Request Body:**
```json
{
  "userRole": "string",
  "description": "string",
  "files": ["string"]
}
```

**Responses:**
- 200: Reply updated successfully
- 400: Bad Request - Missing ID
- 401: Unauthorized
- 404: Reply not found

## Voting System

### Vote on Content
```http
POST /forum/{type}/{id}/vote
```
Adds or removes a vote for a question, answer, or reply.

**Parameters:**
- `type` (path): Type of content ("questions" or "answers")
- `id` (path): Content ID

**Request Body:**
```json
true  // for upvote
false // for downvote
```

**Responses:**
- 200: Vote recorded successfully
- 400: Bad Request - Missing type or ID
- 401: Unauthorized
- 404: Content not found

### Get Vote Count
```http
GET /forum/{type}/{id}/votes
```
Gets the vote count for a question, answer, or reply.

**Parameters:**
- `type` (path): Type of content ("questions" or "answers")
- `id` (path): Content ID

**Responses:**
- 200: Vote count
```json
{
  "votes": integer
}
```
- 400: Bad Request - Missing type or ID

## View System

### Increment Views
```http
POST /forum/questions/{id}/view
```
Increments the view count for a question.

**Parameters:**
- `id` (path): Question ID

**Responses:**
- 200: View count incremented
- 400: Bad Request - Missing ID
- 404: Question not found

### Get View Count
```http
GET /forum/questions/{id}/views
```
Gets the view count for a question.

**Parameters:**
- `id` (path): Question ID

**Responses:**
- 200: View count
```json
{
  "views": integer
}
```
- 400: Bad Request - Missing ID

## User Information

### Get User Information
```http
GET /forum/users/{id}
```
Retrieves information about a student user.

**Implementation Notes:**
- Properly handles MongoDB ObjectId conversion
- Returns complete student information
- Includes error handling for database operations

**Parameters:**
- `id` (path): User ID

**Responses:**
- 200: Student information
```json
{
  "firstName": "string",
  "lastName": "string",
  "userName": "string",
  "email": "string",
  "roles": ["Student"],
  "createdAt": "long",
}
```
- 400: Bad Request - Invalid user ID format
- 404: User not found