/**
 * ═══════════════════════════════════════════════════════════════════════
 * RULE INSPECTOR COMPONENT - View and Manage Drools Rules
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Displays business rules attached to BusinessRuleTasks in the process.
 * Allows users to:
 * - View rule details (DRL, explanation, Java models)
 * - Create new rules via natural language
 * - Activate/deactivate rules
 * - View rule execution history (future)
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */

import { useState, useEffect } from 'react'
import axios from 'axios'
import './RuleInspector.css'

interface RuleSet {
  id: string
  name: string
  description: string
  drl: string
  javaModelsUsed: string[]
  javaModelsCreated: string[]
  explanation: string
  processId: string
  taskId: string
  status: 'DRAFT' | 'VALIDATED' | 'ACTIVE' | 'DEPRECATED'
  createdAt: string
  updatedAt: string
}

interface RuleInspectorProps {
  processId: string | null
  selectedTaskId?: string | null
}

const RuleInspector = ({ processId, selectedTaskId }: RuleInspectorProps) => {
  const [rules, setRules] = useState<RuleSet[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedRule, setSelectedRule] = useState<RuleSet | null>(null)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newRuleName, setNewRuleName] = useState('')
  const [newRuleDescription, setNewRuleDescription] = useState('')
  const [creating, setCreating] = useState(false)

  // Fetch rules for process
  useEffect(() => {
    if (!processId) return

    const fetchRules = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await axios.get(`http://localhost:8080/api/rules/process/${processId}`)
        setRules(response.data)
      } catch (err: any) {
        console.error('[RuleInspector] Failed to fetch rules:', err)
        setError('Failed to load rules')
      } finally {
        setLoading(false)
      }
    }

    fetchRules()
  }, [processId])

  // Filter rules by selected task (if any)
  const displayedRules = selectedTaskId
    ? rules.filter(r => r.taskId === selectedTaskId)
    : rules

  const handleCreateRule = async () => {
    if (!processId || !newRuleName.trim() || !newRuleDescription.trim()) return

    setCreating(true)
    setError(null)

    try {
      const response = await axios.post('http://localhost:8080/api/rules/generate', {
        ruleName: newRuleName.trim(),
        ruleDescription: newRuleDescription.trim(),
        processId: processId,
        taskId: selectedTaskId || null
      })

      const newRule = response.data
      setRules([...rules, newRule])
      setSelectedRule(newRule)
      setShowCreateForm(false)
      setNewRuleName('')
      setNewRuleDescription('')
    } catch (err: any) {
      console.error('[RuleInspector] Failed to create rule:', err)
      setError('Failed to create rule: ' + (err.response?.data?.message || err.message))
    } finally {
      setCreating(false)
    }
  }

  const handleActivateRule = async (ruleId: string) => {
    try {
      const response = await axios.post(`http://localhost:8080/api/rules/${ruleId}/activate`)
      const updatedRule = response.data
      setRules(rules.map(r => r.id === ruleId ? updatedRule : r))
      if (selectedRule?.id === ruleId) {
        setSelectedRule(updatedRule)
      }
    } catch (err: any) {
      console.error('[RuleInspector] Failed to activate rule:', err)
      setError('Failed to activate rule')
    }
  }

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'status-active'
      case 'VALIDATED': return 'status-validated'
      case 'DRAFT': return 'status-draft'
      case 'DEPRECATED': return 'status-deprecated'
      default: return ''
    }
  }

  if (!processId) {
    return (
      <div className="rule-inspector-container">
        <div className="rule-inspector-empty">
          <p>No process selected</p>
          <small>Create or select a process to view rules</small>
        </div>
      </div>
    )
  }

  return (
    <div className="rule-inspector-container">
      <div className="rule-inspector-header">
        <h3>⚙️ Business Rules</h3>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="btn-create-rule"
        >
          {showCreateForm ? '✕ Cancel' : '+ New Rule'}
        </button>
      </div>

      {/* Create Rule Form */}
      {showCreateForm && (
        <div className="create-rule-form">
          <h4>Create New Rule</h4>
          <div className="form-group">
            <label>Rule Name:</label>
            <input
              type="text"
              value={newRuleName}
              onChange={(e) => setNewRuleName(e.target.value)}
              placeholder="e.g., High Value Order Approval"
              disabled={creating}
            />
          </div>
          <div className="form-group">
            <label>Rule Description (Natural Language):</label>
            <textarea
              value={newRuleDescription}
              onChange={(e) => setNewRuleDescription(e.target.value)}
              placeholder="e.g., Orders over $1000 require manager approval"
              rows={4}
              disabled={creating}
            />
          </div>
          <button
            onClick={handleCreateRule}
            disabled={creating || !newRuleName.trim() || !newRuleDescription.trim()}
            className="btn-submit"
          >
            {creating ? '🔄 Generating...' : '🚀 Generate Rule'}
          </button>
        </div>
      )}

      {/* Error Display */}
      {error && <div className="rule-error">{error}</div>}

      {/* Loading State */}
      {loading && <div className="rule-loading">Loading rules...</div>}

      {/* Rules List */}
      {!loading && displayedRules.length === 0 && (
        <div className="rule-empty">
          <p>No rules found</p>
          <small>Create a rule to get started</small>
        </div>
      )}

      {displayedRules.length > 0 && (
        <div className="rules-list">
          {displayedRules.map(rule => (
            <div
              key={rule.id}
              className={`rule-item ${selectedRule?.id === rule.id ? 'selected' : ''}`}
              onClick={() => setSelectedRule(rule)}
            >
              <div className="rule-item-header">
                <h4>{rule.name}</h4>
                <span className={`status-badge ${getStatusBadgeClass(rule.status)}`}>
                  {rule.status}
                </span>
              </div>
              <p className="rule-description">{rule.description}</p>
            </div>
          ))}
        </div>
      )}

      {/* Rule Details Panel */}
      {selectedRule && (
        <div className="rule-details">
          <div className="rule-details-header">
            <h4>{selectedRule.name}</h4>
            {selectedRule.status !== 'ACTIVE' && (
              <button
                onClick={() => handleActivateRule(selectedRule.id)}
                className="btn-activate"
              >
                ✓ Activate
              </button>
            )}
          </div>

          <div className="rule-section">
            <h5>📝 Description</h5>
            <p>{selectedRule.description}</p>
          </div>

          <div className="rule-section">
            <h5>💡 Explanation</h5>
            <p>{selectedRule.explanation}</p>
          </div>

          <div className="rule-section">
            <h5>📄 DRL (Drools Rule Language)</h5>
            <pre className="drl-code">{selectedRule.drl}</pre>
          </div>

          {selectedRule.javaModelsUsed.length > 0 && (
            <div className="rule-section">
              <h5>🔧 Java Models Used</h5>
              <ul>
                {selectedRule.javaModelsUsed.map((model, idx) => (
                  <li key={idx}><code>{model}</code></li>
                ))}
              </ul>
            </div>
          )}

          {selectedRule.javaModelsCreated.length > 0 && (
            <div className="rule-section">
              <h5>📦 Java Models Created</h5>
              <ul>
                {selectedRule.javaModelsCreated.map((model, idx) => (
                  <li key={idx}><code>{model}</code></li>
                ))}
              </ul>
            </div>
          )}

          <div className="rule-meta">
            <small>
              Created: {new Date(selectedRule.createdAt).toLocaleString()}
            </small>
            <small>
              Updated: {new Date(selectedRule.updatedAt).toLocaleString()}
            </small>
          </div>
        </div>
      )}
    </div>
  )
}

export default RuleInspector
