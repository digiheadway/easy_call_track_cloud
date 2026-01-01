interface SettingsProps {
    user: {
        id: number;
        name: string;
        email: string;
        organizationName: string;
        org_id: string; // Updated from organizationId
        role: string;
    };
    onLogout: () => void;
}

export default function Settings({ user, onLogout: _onLogout }: SettingsProps) {
    return (
        <div className="page-container">
            <div className="settings-grid">
                {/* Organization Settings */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Organization Information</h3>
                    </div>
                    <div className="settings-section">
                        <div className="form-group">
                            <label className="form-label">Organization Name</label>
                            <input
                                type="text"
                                className="form-input"
                                defaultValue={user.organizationName}
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Organization ID</label>
                            <div className="input-with-badge">
                                <input
                                    type="text"
                                    className="form-input"
                                    value={user.org_id}
                                    disabled
                                />
                                <span className="badge badge-primary">Verified</span>
                            </div>
                            <p className="form-hint">This ID is used to identify your organization</p>
                        </div>
                        <button className="btn btn-primary">Save Changes</button>
                    </div>
                </div>

                {/* Account Settings */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Account Settings</h3>
                    </div>
                    <div className="settings-section">
                        <div className="form-group">
                            <label className="form-label">Admin Name</label>
                            <input
                                type="text"
                                className="form-input"
                                defaultValue={user.name}
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Email Address</label>
                            <input
                                type="email"
                                className="form-input"
                                defaultValue={user.email}
                            />
                        </div>
                        <button className="btn btn-primary">Update Account</button>
                    </div>
                </div>

                {/* Security Settings */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Security</h3>
                    </div>
                    <div className="settings-section">
                        <div className="form-group">
                            <label className="form-label">Current Password</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder="••••••••"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">New Password</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder="••••••••"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Confirm New Password</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder="••••••••"
                            />
                        </div>
                        <button className="btn btn-primary">Change Password</button>
                    </div>
                </div>

                {/* Preferences */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Preferences</h3>
                    </div>
                    <div className="settings-section">
                        <div className="preference-item">
                            <div>
                                <div className="preference-label">Email Notifications</div>
                                <div className="preference-desc">Receive email updates about activity</div>
                            </div>
                            <label className="toggle">
                                <input type="checkbox" defaultChecked />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>
                        <div className="preference-item">
                            <div>
                                <div className="preference-label">Call Alerts</div>
                                <div className="preference-desc">Get notified about missed calls</div>
                            </div>
                            <label className="toggle">
                                <input type="checkbox" defaultChecked />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>
                        <div className="preference-item">
                            <div>
                                <div className="preference-label">Weekly Reports</div>
                                <div className="preference-desc">Receive weekly summary by email</div>
                            </div>
                            <label className="toggle">
                                <input type="checkbox" />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>
                        <div className="preference-item">
                            <div>
                                <div className="preference-label">Auto-Record Calls</div>
                                <div className="preference-desc">Automatically record all calls</div>
                            </div>
                            <label className="toggle">
                                <input type="checkbox" defaultChecked />
                                <span className="toggle-slider"></span>
                            </label>
                        </div>
                    </div>
                </div>

                {/* Danger Zone */}
                <div className="card danger-zone">
                    <div className="card-header">
                        <h3 className="card-title">Danger Zone</h3>
                    </div>
                    <div className="settings-section">
                        <div className="danger-item">
                            <div>
                                <div className="danger-label">Export All Data</div>
                                <div className="danger-desc">Download all organization data</div>
                            </div>
                            <button className="btn btn-secondary">Export Data</button>
                        </div>
                        <div className="danger-item">
                            <div>
                                <div className="danger-label">Delete Organization</div>
                                <div className="danger-desc">Permanently delete all data and employees</div>
                            </div>
                            <button className="btn btn-danger">Delete Organization</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
