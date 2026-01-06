import { Button } from '@/components/ui/button';
import { ShieldCheck, Phone, Contact as ContactIcon } from 'lucide-react';

interface PermissionDialogProps {
  onAllow: () => void;
}

export default function PermissionDialog({ onAllow }: PermissionDialogProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center p-6 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
        <ShieldCheck className="h-8 w-8 text-primary" />
      </div>
      <h1 className="mt-6 text-2xl font-bold text-foreground">Permissions Required</h1>
      <p className="mt-2 text-muted-foreground">
        To provide you with the best experience, CallSync Notes needs a few permissions.
      </p>
      <div className="mt-8 w-full space-y-4 text-left">
        <div className="flex items-start gap-4 rounded-lg border p-4">
          <Phone className="h-6 w-6 shrink-0 text-primary" />
          <div>
            <h3 className="font-semibold text-foreground">Call Logs</h3>
            <p className="text-sm text-muted-foreground">To display your call history.</p>
          </div>
        </div>
        <div className="flex items-start gap-4 rounded-lg border p-4">
          <ContactIcon className="h-6 w-6 shrink-0 text-primary" />
          <div>
            <h3 className="font-semibold text-foreground">Contacts</h3>
            <p className="text-sm text-muted-foreground">To show names for phone numbers.</p>
          </div>
        </div>
      </div>
      <div className="mt-auto w-full pt-6">
        <Button size="lg" className="w-full" onClick={onAllow}>
          Allow Access
        </Button>
        <Button variant="ghost" size="lg" className="mt-2 w-full" onClick={() => alert("App cannot function without permissions.")}>
          Deny
        </Button>
      </div>
    </div>
  );
}
