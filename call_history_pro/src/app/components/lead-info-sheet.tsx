
'use client';
import { useEffect, useState } from 'react';
import { fetchLeadInfo } from '@/lib/api';
import type { Lead } from '@/lib/types';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { Loader2, AlertCircle } from 'lucide-react';
import { Separator } from '@/components/ui/separator';

interface LeadInfoSheetProps {
  leadId: string;
  isOpen: boolean;
  onClose: () => void;
}

const DetailItem = ({ label, value }: { label: string, value: React.ReactNode }) => {
    if (!value) return null;
    return (
        <div>
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="text-sm font-medium">{value}</p>
        </div>
    )
}

const LeadInfoSheet = ({ leadId, isOpen, onClose }: LeadInfoSheetProps) => {
  const [lead, setLead] = useState<Lead | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && leadId) {
      const getLeadInfo = async () => {
        setLoading(true);
        setError(null);
        setLead(null);
        try {
          const data = await fetchLeadInfo(leadId);
          setLead(data);
        } catch (err) {
          setError('Failed to fetch lead information.');
          console.error(err);
        } finally {
          setLoading(false);
        }
      };
      getLeadInfo();
    }
  }, [isOpen, leadId]);

  return (
    <Sheet open={isOpen} onOpenChange={onClose}>
      <SheetContent side="bottom" className="h-[90dvh]">
        <SheetHeader className="text-left">
          <SheetTitle>Lead Information</SheetTitle>
          {lead && <SheetDescription>Details for {lead.name}</SheetDescription>}
        </SheetHeader>
        <div className="h-[calc(90dvh-100px)] pt-4">
            {loading && (
                <div className="flex h-full items-center justify-center">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
            )}
            {error && (
                <div className="flex h-full flex-col items-center justify-center text-destructive">
                    <AlertCircle className="h-8 w-8" />
                    <p className="mt-2">{error}</p>
                </div>
            )}
            {lead && (
            <ScrollArea className="h-full pr-6">
                <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <DetailItem label="Name" value={lead.name} />
                        <DetailItem label="Phone" value={lead.phone} />
                        <DetailItem label="Alternative Contact" value={lead.alternative_contact_details} />
                        <DetailItem label="Address" value={lead.address} />
                    </div>

                    <Separator />

                    <div className="grid grid-cols-2 gap-4">
                        <DetailItem label="Budget" value={lead.budget} />
                        <DetailItem label="Stage" value={lead.stage} />
                        <DetailItem label="Priority" value={lead.priority} />
                        <DetailItem label="Intent" value={lead.intent} />
                        <DetailItem label="Source" value={lead.source} />
                        <DetailItem label="Segment" value={lead.segment} />
                        <DetailItem label="Assigned To" value={lead.assigned_to} />
                    </div>
                     
                    <Separator />
                    
                    <DetailItem label="Note" value={<p className="whitespace-pre-wrap">{lead.note}</p>} />
                    <DetailItem label="About" value={<p className="whitespace-pre-wrap">{lead.about_him}</p>} />
                    <DetailItem label="Requirement" value={<p className="whitespace-pre-wrap">{lead.requirement_description}</p>} />

                    <Separator />

                    <div>
                        <p className="text-xs text-muted-foreground mb-2">Tags</p>
                        <div className="flex flex-wrap gap-2">
                        {lead.tags && lead.tags.split(',').map(tag => (
                            <Badge key={tag} variant="secondary">{tag}</Badge>
                        ))}
                        </div>
                    </div>
                     
                    <Separator />
                    
                    <div>
                        <p className="text-sm font-medium mb-2">Tasks</p>
                        {lead.tasks.length > 0 ? (
                            <div className="space-y-2">
                                {lead.tasks.map((task: any, index: number) => (
                                    <div key={index} className="rounded-md border p-2 text-sm">
                                        <p>{task.description}</p>
                                        <p className="text-xs text-muted-foreground">{task.due_date}</p>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground">No tasks found.</p>
                        )}
                    </div>
                </div>
            </ScrollArea>
            )}
        </div>
      </SheetContent>
    </Sheet>
  );
};

export default LeadInfoSheet;
