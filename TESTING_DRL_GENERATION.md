# Testing DRL Generation & Properties Panel Integration

**Date**: 2026-01-18  
**Backend Status**: ✅ Running with latest changes (restarted at 08:55:26)

---

## 🧪 Test Scenario: Leave Approval Process

### Step 1: Open the Frontend

1. Navigate to: `http://localhost:5173`
2. Open the **AI Assistant** panel (chat icon)

### Step 2: Create a Leave Approval Process

**Input the following prompt**:
```
Create a simple leave approval process where an employee submits a request, 
their manager reviews it, and if approved, HR processes it. Reject if leave 
is more than 5 days or during peak delivery period.
```

### Step 3: Wait for Process Generation

- The AI will analyze the description
- Generate BPMN with a **Business Rule Task** for validation
- Create detailed rule metadata
- Inject documentation into the BPMN JSON

**Expected Console Logs** (Backend):
```
[ProcessReasonerService] Created RuleModel: id=RejectIfMoreThan5Days, expression=days > 5, action=reject
[ProcessReasonerService] Created RuleModel: id=RejectIfPeakPeriod, expression=during peak delivery period, action=reject
[ProcessReasonerService] Created RuleModel: id=ApproveIfAllCriteriaMet, expression=all criteria met, action=approve
[ProcessReasonerService] Added documentation and Camunda attributes to BusinessRuleTask: Task_ValidateLeave
```

### Step 4: Inspect the BPMN Diagram

**Expected Elements**:
1. ✅ **Start Event**: "Leave Request Submitted"
2. ✅ **User Task**: "Submit Leave Request"
3. ✅ **Business Rule Task**: "Validate Leave Request" (🔍 **This is the key element**)
4. ✅ **Exclusive Gateway**: "Validation Result"
5. ✅ **User Task**: "Manager Approval"
6. ✅ **User Task**: "HR Approval"
7. ✅ **End Events**: "Leave Approved" and "Leave Rejected"

### Step 5: Select the Business Rule Task

1. Click on the **"Validate Leave Request"** task in the diagram
2. The **Properties Panel** should appear on the right side

### Step 6: Verify Properties Panel Content

#### **General Tab**
- **Name**: "Validate Leave Request"
- **ID**: "Task_ValidateLeave" (or similar)
- **Type**: "Business Rule Task"

#### **Documentation Tab** ⭐ **KEY VERIFICATION**
You should see:
```
Business Rules:

1. IF days > 5 THEN reject with reason 'Leave request exceeds 5 day limit'
2. IF during peak delivery period THEN reject with reason 'Overlaps with critical business delivery dates'
3. IF all criteria met THEN approve with reason 'Leave request is valid'

DRL File: LeaveValidationRules.drl
```

#### **Camunda Platform Tab** (if visible)
- **Implementation**: DMN or Business Rule
- **Decision Ref**: "LeaveValidationRules"
- **Result Variable**: "validationResult"
- **Map Decision Result**: "singleResult"
- **DMN Resource**: "LeaveValidationRules.drl"

---

## ✅ Success Criteria

### Backend Verification
- [x] Backend restarted successfully with new code
- [x] No compilation errors
- [x] ProcessReasonerService logs show rule creation
- [x] Documentation injection logs appear

### Frontend Verification
- [ ] Business Rule Task is generated (not multiple gateways)
- [ ] Properties panel shows "Documentation" tab
- [ ] Documentation tab displays all business rules
- [ ] Rules are formatted correctly with IF/THEN structure
- [ ] DRL file name is mentioned

### Data Verification
- [ ] ProcessModel in database contains RuleModel objects
- [ ] Each rule has: id, expression, description, ruleType, priority
- [ ] BPMN JSON includes `documentation` field on BusinessRuleTask
- [ ] Camunda attributes are present in BPMN JSON

---

## 🐛 Troubleshooting

### Issue: Properties Panel is Empty
**Solution**: Make sure you've selected the Business Rule Task (it should be highlighted)

### Issue: Documentation Tab is Missing
**Possible Causes**:
1. The AI didn't inject the documentation field
2. The BPMN JSON structure is different than expected
3. The properties panel isn't showing all tabs

**Debug Steps**:
1. Check backend logs for "Added documentation to BusinessRuleTask"
2. Export the BPMN XML and search for `<bpmn:documentation>`
3. Verify the frontend is using the latest `CamundaPlatformPropertiesProviderModule`

### Issue: Multiple Gateways Instead of Business Rule Task
**Possible Causes**:
1. The AI prompt wasn't updated correctly
2. The AI is still using the old pattern

**Solution**:
1. Verify `ProcessReasonerService.createReasoningPrompt()` includes the new instructions
2. Check that the backend restarted after the code changes
3. Try a different process description with explicit "business rules" mention

### Issue: DRL File Not Generated
**Possible Causes**:
1. Rules weren't added to ProcessModel
2. DrlGeneratorService wasn't called
3. Publishing step wasn't triggered

**Debug Steps**:
1. Check if `ProcessModel.getRules()` contains RuleModel objects
2. Verify `ProcessPublishingService.publishProcess()` is called
3. Check `backend/data/kogito/rules/` directory for DRL files

---

## 📊 Expected vs Actual Comparison

### Before (Option B - Documentation Field Only)
```
Documentation Tab:
"Business rules are applied to validate the leave request."
```

### After (Option A - Full DRL Generation)
```
Documentation Tab:
"Business Rules:

1. IF days > 5 THEN reject with reason 'Leave request exceeds 5 day limit'
2. IF during peak delivery period THEN reject with reason 'Overlaps with critical business delivery dates'
3. IF all criteria met THEN approve with reason 'Leave request is valid'

DRL File: LeaveValidationRules.drl"
```

---

## 🎯 Next Steps After Successful Testing

1. **Verify DRL File Generation**:
   - Navigate to `backend/data/kogito/rules/`
   - Find the generated DRL file (e.g., `proc_xxx.drl`)
   - Verify it contains all rules with correct Drools syntax

2. **Test Rule Execution** (if Kogito is fully configured):
   - Start a process instance
   - Provide test data (e.g., `days: 6`)
   - Verify the rule fires and rejects the request

3. **Test with Different Process Descriptions**:
   - Try a different domain (e.g., loan approval, order processing)
   - Verify the AI generates appropriate business rules
   - Check that documentation is always injected

4. **Explore Properties Panel Customization**:
   - Consider adding a dedicated "Business Rules" tab
   - Implement inline rule editing
   - Add rule testing capabilities

---

## 📝 Test Results Log

**Date**: _____________  
**Tester**: _____________

| Test Step | Status | Notes |
|-----------|--------|-------|
| Frontend loads | ⬜ Pass / ⬜ Fail | |
| Process creation prompt accepted | ⬜ Pass / ⬜ Fail | |
| Business Rule Task generated | ⬜ Pass / ⬜ Fail | |
| Properties panel opens | ⬜ Pass / ⬜ Fail | |
| Documentation tab visible | ⬜ Pass / ⬜ Fail | |
| Rules text is correct | ⬜ Pass / ⬜ Fail | |
| DRL file name mentioned | ⬜ Pass / ⬜ Fail | |
| Camunda attributes present | ⬜ Pass / ⬜ Fail | |
| Backend logs show rule creation | ⬜ Pass / ⬜ Fail | |
| DRL file generated | ⬜ Pass / ⬜ Fail | |

**Overall Result**: ⬜ **PASS** / ⬜ **FAIL**

**Comments**:
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

---

**Ready to test!** 🚀 Open the frontend and try creating the leave approval process.
